import {hasClassOrParentHasClass} from "./utils";
import {getTreePathOfElement, handleTabPressed, setNextFocusElement} from "./interface";
import {getTree} from "./treeManipulation";
import {handleExprSelectorChoice} from "./actions";

const SELECTOR_FOCUS_CLASS = 'focused';
const DROPDOWN_VISIBLE_CLASS = 'show';
const OPTION_HIDDEN_CLASS = 'hidden';
const OPTION_HIGHLIGHT_CLASS = 'highlight';
const UP_ARROW = '&#9650;';
const DOWN_ARROW = '&#9660;';


class CustomExprSelector {
    protected readonly container: HTMLDivElement;
    protected readonly input: HTMLInputElement;
    private readonly button: HTMLButtonElement;
    private readonly dropdown: HTMLDivElement;
    private readonly options: HTMLLIElement[];

    constructor(container: HTMLDivElement) {
        this.container = container;
        this.input = container.querySelector('.expr-selector-input') as HTMLInputElement;
        this.button = container.querySelector('.expr-selector-button') as HTMLButtonElement;
        this.dropdown = container.querySelector('.expr-selector-dropdown') as HTMLDivElement;
        this.options = Array.from(this.dropdown.querySelectorAll('ul > li')) as HTMLLIElement[];

        this.setup();
    }

    private setup(): void {
        this.setupListeners();
        this.button.tabIndex = -1;
    }

    private setupListeners(): void {
        this.input.addEventListener('input', () => this.updateDropdown());
        this.input.addEventListener('keydown', evt => this.handleKeydown(evt));
        this.input.addEventListener('focus', () => this.showDropdown());
        this.input.addEventListener('blur', () => this.handleBlur());

        this.button.addEventListener('click', () => this.toggleDropdown());

        this.dropdown.addEventListener('mousedown', () => this.setInteractingWithDropdown(true));
        this.dropdown.addEventListener('mouseup', () => this.setInteractingWithDropdown(false));
        this.dropdown.addEventListener('wheel', evt => this.handleDropdownWheel(evt));

        this.setupOptionListeners();
    }

    private setupOptionListeners(): void {
        this.options.forEach(option => {
            if (!(option instanceof HTMLLIElement)) {
                throw new Error('Option was not an HTMLLIElement');
            }
            option.addEventListener('mousedown', evt => {
                evt.preventDefault();
                this.selectOption(option);
            });
        });
    }

    protected updateDropdown(): void {
        const filterText = this.input.value.toLowerCase();
        this.options.forEach((option) => {
            const matches = option.innerText.toLowerCase().includes(filterText);
            matches ? this.showOption(option) : this.hideOption(option);
        });
        this.highlightOption(0);
    }

    private handleKeydown(event: KeyboardEvent) {
        if (event.key === 'Enter') {
            this.selectHighlightedOption();
        } else if (event.key === 'ArrowDown') {
            this.moveHighlight(1);
        } else if (event.key === 'ArrowUp') {
            this.moveHighlight(-1);
        } else if (event.key === 'Tab') {
            handleTabPressed(event);
        }
    }

    private handleBlur() {
        setTimeout(() => {
            if (this.isDropdownInteracting) {
                setTimeout(() => this.input.focus(), 0);
            } else {
                this.hideDropdown();
            }
        });
    }

    protected toggleDropdown() {
        if (this.isDropdownVisible()) {
            this.hideDropdown();
        } else {
            this.showDropdown();
        }
    }

    protected showDropdown() {
        this.dropdown.classList.add(DROPDOWN_VISIBLE_CLASS);
        this.dropdown.scrollTop = 0;
        this.button.innerHTML = UP_ARROW;
        this.container.classList.add(SELECTOR_FOCUS_CLASS);
        this.highlightOption(0);
    }

    protected hideDropdown() {
        this.dropdown.classList.remove(DROPDOWN_VISIBLE_CLASS);
        this.button.innerHTML = DOWN_ARROW;
        this.container.classList.remove(SELECTOR_FOCUS_CLASS);
    }

    private isDropdownVisible() {
        return this.dropdown.classList.contains(DROPDOWN_VISIBLE_CLASS);
    }

    private showOption(option: HTMLLIElement) {
        option.classList.remove(OPTION_HIDDEN_CLASS);
    }

    private hideOption(option: HTMLLIElement) {
        option.classList.add(OPTION_HIDDEN_CLASS);
    }

    private highlightOption(index: number) {
        if (!this.isDropdownVisible()) {
            this.showDropdown();
        }

        const options = this.getVisibleOptions();
        options.forEach((opt) => opt.classList.remove(OPTION_HIGHLIGHT_CLASS));
        const option = options[index];
        if (option) {
            option.classList.add(OPTION_HIGHLIGHT_CLASS);
            option.scrollIntoView({block: 'nearest'});
        }
    }

    private moveHighlight(offset: number) {
        const options = this.getVisibleOptions();
        const currentIndex = this.getSelectedIndex();
        const newIndex = (currentIndex + offset + options.length) % options.length;
        this.highlightOption(newIndex);
    }

    private selectHighlightedOption() {
        const highlighted = this.getSelectedOption();
        if (highlighted) {
            setNextFocusElement(this.container);
            this.selectOption(highlighted);
        }
    }

    protected selectOption(option: HTMLLIElement) {
        this.input.value = option.innerText;
        handleExprSelectorChoice(this.container, option.getAttribute('data-value'));
        this.hideDropdown();
    }

    private getVisibleOptions(): HTMLLIElement[] {
        return this.options.filter((opt) => !opt.classList.contains(OPTION_HIDDEN_CLASS));
    }

    private isDropdownInteracting = false;

    private setInteractingWithDropdown(value: boolean) {
        this.isDropdownInteracting = value;
    }

    private getSelectedIndex(): number {
        return this.getVisibleOptions().findIndex((opt) =>
            opt.classList.contains(OPTION_HIGHLIGHT_CLASS)
        );
    }

    private getSelectedOption(): HTMLLIElement | undefined {
        return this.getVisibleOptions().find((opt) =>
            opt.classList.contains(OPTION_HIGHLIGHT_CLASS)
        );
    }

    private handleDropdownWheel(evt: WheelEvent) {
        if (!((this.dropdown.scrollTop === 0 && evt.deltaY < 0)
            || (this.dropdown.scrollTop === this.dropdown.scrollHeight - this.dropdown.clientHeight && evt.deltaY > 0))) {
            evt.stopPropagation();
        }
    }
}


let exprSelectors: CustomExprSelector[] = [];


export function replaceSelectInputs(): void {
    exprSelectors = [];

    const selectInputs: NodeListOf<HTMLSelectElement> = getTree().querySelectorAll(
        'select.expr-dropdown[data-tree-path]:not([disabled]), select.type-dropdown[data-tree-path]:not([disabled])'
    );
    selectInputs.forEach(select => {
        if (hasClassOrParentHasClass(select, 'phantom')) {
            return;
        }

        const options = Array.from(select.options).slice(1);
        const treePath = getTreePathOfElement(select);
        let placeholderText: string;
        let kind: string;
        if (select.classList.contains('expr-dropdown')) {
            placeholderText = 'Enter Expression...';
            kind = 'expr';
        } else {
            placeholderText = 'Enter Type...';
            kind = 'type';
        }
        select.outerHTML = createExprSelectorHTML(treePath, kind, placeholderText, options);

        const newSelector = getTree().querySelector(`.expr-selector-container[data-tree-path="${treePath}"]`) as HTMLDivElement;
        exprSelectors.push(setupTermSelector(newSelector));
    });

    replaceDisabledSelectInputs();
}

function replaceDisabledSelectInputs(): void {
    const selectInputs: NodeListOf<HTMLSelectElement> = getTree().querySelectorAll(
        'select.expr-dropdown:disabled, select.type-dropdown:disabled, .phantom select.expr-dropdown, .phantom select.type-dropdown'
    );

    function createDisabledSelectHTML(select: HTMLSelectElement, treePath: string): string {
        const kind = select.classList.contains('expr-dropdown') ? 'Expression' : 'Type';
        return `<div class="expr-selector-placeholder" data-tree-path="${treePath}">Unspecified ${kind}</div>`;
    }

    selectInputs.forEach(select => {
        const treePath = getTreePathOfElement(select);
        select.outerHTML = createDisabledSelectHTML(select, treePath);
    });
}

function createExprSelectorHTML(treePath: string, kind: string, placeholderText: string, options: HTMLOptionElement[]): string {
    const optionsList: string[] = options.map(createExprSelectorOptionHtml);
    return `<div class="expr-selector-container" data-tree-path="${treePath}" data-kind="${kind}">
              <input type="text" class="expr-selector-input" placeholder="${placeholderText}" data-tree-path="${treePath}" />
              <button class="expr-selector-button">${UP_ARROW}</button>
              <div class="expr-selector-dropdown">
                <ul>
                ${optionsList.join('')}
                </ul>
              </div>
            </div>`;
}

function createExprSelectorOptionHtml(optionElement: HTMLOptionElement): string {
    const aliases: string = optionElement.getAttribute('data-aliases') || '';
    return `<li data-value="${optionElement.value}" data-aliases="${aliases}">${optionElement.innerHTML}</li>`;
}

function setupTermSelector(termSelectorContainer: HTMLDivElement): CustomExprSelector {
    return new CustomExprSelector(termSelectorContainer);
}

class ExampleExprSelector extends CustomExprSelector {
    private readonly output: HTMLDivElement;

    constructor(container: HTMLDivElement, output: HTMLDivElement) {
        super(container);
        this.output = output;
    }

    protected override selectOption(option: HTMLLIElement) {
        this.input.value = option.innerText;
        this.output.textContent = option.textContent;
        this.hideDropdown();
        this.input.focus();
        this.container.classList.add(SELECTOR_FOCUS_CLASS);
    }

    private clearOutput() {
        this.output.textContent = '?';
    }

    protected override updateDropdown() {
        super.updateDropdown();
        this.clearOutput();
    }
}

export function setupExampleSelector(termSelectorContainer: HTMLDivElement): void {
    new ExampleExprSelector(termSelectorContainer, document.getElementById("expr-selector-output") as HTMLDivElement);
}
