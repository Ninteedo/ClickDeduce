import {getSelectedMode, hasClassOrParentHasClass} from "../utils";
import {getCurrentLanguage, getCurrentNodeString} from "../treeManipulation";
import {runAction} from "../actions";
import {BaseDropdownSelector, DropdownOption} from "./baseDropdownSelector";
import {getTree, getTreePathOfElement} from "../globals/elements";
import {getExprParsePreviewHtml, parseExprText} from "../serverRequest";
import {RulePreview} from "./rulePreview";
import {ParsePreview} from "./parsePreview";

const UP_ARROW = '&#9650;';
const DOWN_ARROW = '&#9660;';


export class CustomExprSelector extends BaseDropdownSelector {
    private readonly button: HTMLButtonElement;

    protected readonly rulePreview: RulePreview;
    protected readonly parsePreview: ParsePreview;

    constructor(container: HTMLDivElement) {
        super(container, '.expr-selector-input', '.expr-selector-dropdown', 'ul > li');
        this.button = container.querySelector('.expr-selector-button') as HTMLButtonElement;
        this.setup();
        this.rulePreview = new RulePreview(container);
        this.parsePreview = new ParsePreview(container);
        this.updateWidth();
    }

    private setup(): void {
        this.button.tabIndex = -1;
        this.button.addEventListener('click', () => this.toggleDropdown());
    }

    override setupOptionListener(option: DropdownOption): void {
        super.setupOptionListener(option);
        option.element.addEventListener('mouseenter', () => {
            this.rulePreview.show(option.getValue());
        });
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
        this.rulePreview.show(option.getValue());
    }

    protected override clearOptionHighlight(): void {
        super.clearOptionHighlight();
        this.rulePreview.hide();
    }

    protected override updateDropdown(): void {
        super.updateDropdown();

        this.updateWidth();

        const parseHtml = getExprParsePreviewHtml(
            getCurrentLanguage(),
            this.input.value,
            getSelectedMode(),
            getCurrentNodeString()!,
            this.getTreePath()
        );
        if (parseHtml) {
            this.parsePreview.show(parseHtml);
        } else {
            this.parsePreview.hide();
        }
    }

    protected override handleBlur(): void {
        super.handleBlur();
        this.rulePreview.hide();
        this.parsePreview.hide();
    }

    protected override enterPressed(): void {
        if (!this.getSelectedOption()) {
            const parsedExpr = parseExprText(getCurrentLanguage(), this.input.value);
            if (parsedExpr) {
                runAction("ParseExprAction", this.getTreePath(), this.input.value);
                return;
            }
        }
        super.enterPressed();
    }

    protected override postSelectOption(value: string): void {
        const actionName = this.isTypeSelector() ? "SelectTypeAction" : "SelectExprAction";
        runAction(actionName, this.getTreePath(), value);
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


let exprSelectors: CustomExprSelector[] = [];


export function replaceSelectInputs(): CustomExprSelector[] {
    exprSelectors = [];

    const selectInputs: NodeListOf<HTMLSelectElement> = getTree().querySelectorAll(
        'select.expr-dropdown[data-tree-path]:not([disabled]), select.type-dropdown[data-tree-path]:not([disabled])'
    );
    selectInputs.forEach(select => {
        if (!hasClassOrParentHasClass(select, 'phantom')) {
            exprSelectors.push(createExprSelector(select));
        }
    });

    replaceDisabledSelectInputs();

    return exprSelectors;
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
