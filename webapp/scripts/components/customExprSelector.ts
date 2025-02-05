import {getSelectedMode} from "../utils";
import {getCurrentLanguage, getCurrentNodeString} from "../treeManipulation";
import {runAction} from "../actions";
import {BaseDropdownSelector, DropdownOption, NameDropdownOption} from "./baseDropdownSelector";
import {getTree, getTreePathOfElement} from "../globals/elements";
import {getExprParsePreviewHtml} from "../serverRequest";
import {RulePreview} from "./rulePreview";
import {ParsePreview} from "./parsePreview";

const UP_ARROW = '&#9650;';
const DOWN_ARROW = '&#9660;';


export class CustomExprSelector extends BaseDropdownSelector {
    private readonly button: HTMLButtonElement;

    protected readonly rulePreview: RulePreview;
    protected readonly parsePreview: ParsePreview | undefined;

    constructor(container: HTMLDivElement) {
        const dropdown = container.querySelector('.expr-selector-dropdown') as HTMLDivElement;
        const dropdownList = dropdown.querySelector('ul') as HTMLUListElement;
        const options: DropdownOption[] = Array.from(dropdownList.querySelectorAll('li'))
            .map(option => new NameDropdownOption(option as HTMLLIElement));

        const parseOptionElement = document.createElement('li');
        parseOptionElement.innerText = 'Parsed...';
        const parseDropdownOption = new ParseDropdownOption(parseOptionElement, container.getAttribute('data-tree-path')!);
        options.push(parseDropdownOption);

        dropdownList.replaceChildren(...options.map(option => option.element));

        super(
            container,
            container.querySelector('.expr-selector-input')!,
            dropdown,
            options
        );
        this.button = container.querySelector('.expr-selector-button') as HTMLButtonElement;
        this.setup();
        this.rulePreview = new RulePreview(container);
        if (!this.isTypeSelector()) {
            this.parsePreview = new ParsePreview(container);
        } else {
            parseDropdownOption.disable();
        }
        this.updateWidth();
    }

    private setup(): void {
        this.button.tabIndex = -1;
        this.button.addEventListener('click', () => this.toggleDropdown());
    }

    override setupOptionListener(option: DropdownOption): void {
        super.setupOptionListener(option);
        if (option instanceof NameDropdownOption) {
            option.element.addEventListener('mouseenter', () => {
                this.rulePreview.show(option.getValue());
            });
        }
    }

    protected override showDropdown(): void {
        super.showDropdown();
        this.button.innerHTML = UP_ARROW;
    }

    protected override hideDropdown(): void {
        super.hideDropdown();
        this.button.innerHTML = DOWN_ARROW;
        this.rulePreview.hide();
    }

    protected override setOptionHighlight(option: DropdownOption): void {
        super.setOptionHighlight(option);
        if (option instanceof NameDropdownOption) {
            this.rulePreview.show(option.getValue());
        }
    }

    protected selectOption(option: DropdownOption): void {
        super.selectOption(option);
        if (option instanceof ParseDropdownOption) {
            this.postSelectOption(option);
        }
    }

    protected override clearOptionHighlight(): void {
        super.clearOptionHighlight();
        this.rulePreview.hide();
    }

    protected override updateDropdown(): void {
        super.updateDropdown();

        this.updateWidth();

        if (this.parsePreview) {
            const [errorIndex, res] = getExprParsePreviewHtml(
                getCurrentLanguage(),
                this.input.value,
                getSelectedMode(),
                getCurrentNodeString()!,
                this.getTreePath()
            );
            if (errorIndex < 0) {
                this.parsePreview.show(res);
            } else if (this.input.value) {
                this.parsePreview.showError(res, errorIndex);
            } else {
                this.parsePreview.hide();
            }
        }
    }

    protected override handleBlur(): void {
        super.handleBlur();
        this.rulePreview.hide();
        this.parsePreview?.hide();
    }

    protected override postSelectOption(option: DropdownOption): void {
        if (option instanceof NameDropdownOption) {
            const actionName = this.isTypeSelector() ? "SelectTypeAction" : "SelectExprAction";
            runAction(actionName, this.getTreePath(), option.getValue());
        } else if (option instanceof ParseDropdownOption) {
            runAction("ParseExprAction", this.getTreePath(), this.input.value);
        } else {
            throw new Error(`Unexpected option type: ${option}`);
        }
    }

    override disable() {
        super.disable();
        this.button.setAttribute('disabled', 'true');
    }

    override enable() {
        super.enable();
        this.button.removeAttribute('disabled');
    }

    isTypeSelector(): boolean {
        return this.container.getAttribute('data-kind') === 'type';
    }

    private updateWidth(): void {
        this.input.style.width = `max(${this.input.placeholder.length + 1}ch, ${this.input.value.length + 1}ch)`;
    }
}

class ParseDropdownOption extends DropdownOption {
    private readonly treePath: string;

    private disabled: boolean = false;

    constructor(element: HTMLLIElement, treePath: string) {
        super(element);
        this.treePath = treePath;
    }

    override shouldShow(filter: string): boolean {
        if (this.disabled) return false;

        const [errorIndex, res] = getExprParsePreviewHtml(
            getCurrentLanguage(),
            filter,
            getSelectedMode(),
            getCurrentNodeString()!,
            this.treePath
        );
        return errorIndex < 0 && res.length > 0;
    }

    disable(): void {
        this.disabled = true;
    }
}

export function createExprSelector(select: HTMLSelectElement): CustomExprSelector {
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
    return setupTermSelector(newSelector);
}

export function replaceDisabledSelectInputs(element: HTMLElement | undefined = undefined): void {
    if (!element) {
        element = getTree();
    }

    const selectInputs: NodeListOf<HTMLSelectElement> = element.querySelectorAll(
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

    protected override postSelectOption(option: DropdownOption) {
        if (option instanceof NameDropdownOption) {
            this.output.textContent = option.getValue();
            this.input.focus();
            this.container.classList.add(this.SELECTOR_FOCUS_CLASS);
        } else {
            throw new Error(`Unexpected option type: ${option}`);
        }
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
