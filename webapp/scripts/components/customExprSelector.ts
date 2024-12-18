import {hasClassOrParentHasClass} from "../utils";
import {getTreePathOfElement} from "../interface";
import {getTree} from "../treeManipulation";
import {handleExprSelectorChoice} from "../actions";
import {BaseDropdownSelector} from "./baseDropdownSelector";

const UP_ARROW = '&#9650;';
const DOWN_ARROW = '&#9660;';


export class CustomExprSelector extends BaseDropdownSelector {
    private readonly button: HTMLButtonElement;

    constructor(container: HTMLDivElement) {
        super(container, '.expr-selector-input', '.expr-selector-dropdown', 'ul > li');
        this.button = container.querySelector('.expr-selector-button') as HTMLButtonElement;
        this.setup();
    }

    private setup(): void {
        this.button.tabIndex = -1;
        this.button.addEventListener('click', () => this.toggleDropdown());
    }

    protected override showDropdown() {
        super.showDropdown();
        this.button.innerHTML = UP_ARROW;
    }

    protected override hideDropdown() {
        super.hideDropdown();
        this.button.innerHTML = DOWN_ARROW;
    }

    protected override postSelectOption(value: string) {
        handleExprSelectorChoice(this.container, value);
    }

    isTypeSelector(): boolean {
        return this.container.getAttribute('data-kind') === 'type';
    }
}


let exprSelectors: CustomExprSelector[] = [];


export function replaceSelectInputs(): CustomExprSelector[] {
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

    return exprSelectors;
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

    protected override postSelectOption(value: string) {
        this.output.textContent = value;
        this.input.focus();
        this.container.classList.add(this.SELECTOR_FOCUS_CLASS);
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
