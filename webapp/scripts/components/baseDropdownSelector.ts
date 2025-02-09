import {handleTabPressed} from "../focus";
import {stripTooltip} from "../utils";
import {AbstractTreeInput} from "./abstractTreeInput";
import {ClassDict} from "../globals/classDict";

export class BaseDropdownSelector implements AbstractTreeInput {
    protected readonly container: HTMLDivElement;
    protected readonly input: HTMLInputElement;
    protected readonly dropdown: HTMLDivElement;
    readonly options: DropdownOption[];

    constructor(container: HTMLDivElement, input: HTMLInputElement, dropdown: HTMLDivElement, options: DropdownOption[]) {
        this.container = container;
        this.input = input;
        this.dropdown = dropdown;
        this.options = options;

        this.container.classList.add(ClassDict.DROPDOWN_SELECTOR_CONTAINER);
        this.dropdown.classList.add(ClassDict.DROPDOWN);

        this.setupListeners();
    }

    focus(): void {
        this.input.focus();
        this.input.select();
    }

    blur(): void {
        this.input.blur();
    }

    disable(): void {
        this.input.disabled = true;
        this.input.readOnly = true;
    }

    enable(): void {
        this.input.disabled = false;
        this.input.readOnly = false;
    }

    getTreePath(): string {
        const treePath = this.container.getAttribute('data-tree-path') ?? this.input.getAttribute('data-tree-path');
        if (treePath === null) throw new Error('Dropdown selector does not have a data-tree-path attribute.');
        return treePath;
    }

    protected setupListeners(): void {
        this.input.addEventListener('input', () => this.updateDropdown());
        this.input.addEventListener('keydown', event => this.handleKeydown(event));
        this.input.addEventListener('focus', () => this.showDropdown());
        this.input.addEventListener('blur', () => this.handleBlur());

        this.dropdown.addEventListener('mousedown', () => this.setInteractingWithDropdown(true));
        this.dropdown.addEventListener('mouseup', () => this.setInteractingWithDropdown(false));
        this.dropdown.addEventListener('wheel', evt => this.handleDropdownWheel(evt));

        this.setupOptionListeners();
    }

    private setupOptionListeners(): void {
        this.options.forEach(option => this.setupOptionListener(option));
    }

    protected setupOptionListener(option: DropdownOption): void {
        option.element.addEventListener('click', evt => {
            evt.preventDefault();
            this.selectOption(option);
        });
    }

    protected selectOption(option: DropdownOption): void {
        if (option instanceof NameDropdownOption) {
            this.input.value = option.getValue();
            this.input.dispatchEvent(new Event('input'));
            this.hideDropdown();
            this.input.dispatchEvent(new Event('change'));
            this.postSelectOption(option);
        }
    }

    enterValue(value: string): void {
        this.input.value = value;
        this.updateDropdown();
        const option = this.getSelectedOption();
        this.hideDropdown();
        if (option) {
            this.postSelectOption(option);
        }
    }

    protected postSelectOption(_option: DropdownOption): void {}

    protected updateDropdown(): void {
        const filterText = this.input.value.toLowerCase();
        this.options.forEach(option => {
            option.shouldShow(filterText) ? option.show() : option.hide();
        });
        this.highlightOption(0);
    }

    protected handleKeydown(event: KeyboardEvent): void {
        if (event.key === 'Enter') {
            this.enterPressed();
        } else if (event.key === 'ArrowDown') {
            this.moveHighlight(1);
        } else if (event.key === 'ArrowUp') {
            this.moveHighlight(-1);
        } else if (event.key === 'Tab') {
            handleTabPressed(event);
        }
    }

    protected handleBlur(): void {
        setTimeout(() => {
            if (this.isDropdownInteracting) {
                setTimeout(() => this.input.focus(), 0);
            } else {
                this.hideDropdown();
            }
        });
    }

    protected toggleDropdown(): void {
        if (this.isDropdownVisible()) {
            this.hideDropdown();
        } else {
            this.showDropdown();
        }
    }

    protected showDropdown(): void {
        if (this.isDropdownVisible()) return;
        this.dropdown.classList.add(ClassDict.SHOW);
        this.dropdown.scrollTop = 0;
        this.container.classList.add(ClassDict.FOCUSED);
        this.updateDropdown();
    }

    protected hideDropdown(): void {
        if (!this.isDropdownVisible()) return;
        this.dropdown.classList.remove(ClassDict.SHOW);
        this.container.classList.remove(ClassDict.FOCUSED);
    }

    private isDropdownVisible(): boolean {
        return this.dropdown.classList.contains(ClassDict.SHOW);
    }

    private moveHighlight(offset: number): void {
        const options = this.getVisibleOptions();
        const currentIndex = this.getSelectedIndex();
        const newIndex = (currentIndex + offset + options.length) % options.length;
        this.highlightOption(newIndex);
    }

    private highlightOption(index: number): void {
        const options = this.getVisibleOptions();
        options.forEach(option => option.removeHighlight());
        const option = options[index];
        if (option) {
            this.setOptionHighlight(option);
        } else {
            this.clearOptionHighlight();
        }
    }

    protected setOptionHighlight(option: DropdownOption): void {
        option.highlight();
    }

    protected clearOptionHighlight(): void {}

    protected enterPressed(): void {
        const highlighted = this.getSelectedOption();
        if (highlighted) {
            this.selectOption(highlighted);
        }
    }

    private getVisibleOptions(): DropdownOption[] {
        return this.options.filter((option) => !option.isHidden());
    }

    private getSelectedIndex(): number {
        return this.getVisibleOptions().findIndex((option) => option.isHighlighted());
    }

    protected getSelectedOption(): DropdownOption | undefined {
        return this.getVisibleOptions().find(option => option.isHighlighted());
    }

    private handleDropdownWheel(evt: WheelEvent): void {
        if (!((this.dropdown.scrollTop === 0 && evt.deltaY < 0)
            || (this.dropdown.scrollTop === this.dropdown.scrollHeight - this.dropdown.clientHeight && evt.deltaY > 0))) {
            evt.stopPropagation();
        }
    }

    private isDropdownInteracting = false;

    private setInteractingWithDropdown(value: boolean) {
        this.isDropdownInteracting = value;
    }
}

export abstract class DropdownOption {
    public readonly element: HTMLLIElement;

    protected constructor(option: HTMLLIElement) {
        this.element = option;
    }

    public show(): void {
        this.element.classList.remove(ClassDict.HIDDEN);
    }

    public hide(): void {
        this.element.classList.add(ClassDict.HIDDEN);
    }

    public highlight(): void {
        this.element.classList.add(ClassDict.HIGHLIGHT);
        this.element.scrollIntoView({block: 'nearest'});
    }

    public removeHighlight(): void {
        this.element.classList.remove(ClassDict.HIGHLIGHT);
    }

    public isHidden(): boolean {
        return this.element.classList.contains(ClassDict.HIDDEN);
    }

    public isHighlighted(): boolean {
        return this.element.classList.contains(ClassDict.HIGHLIGHT);
    }

    public abstract shouldShow(inputValue: string): boolean;
}

export class NameDropdownOption extends DropdownOption {
    constructor(option: HTMLLIElement) {
        super(option);
        this.element.querySelectorAll(`.${ClassDict.TOOLTIP}`).forEach(tooltipDiv => stripTooltip(tooltipDiv));
    }

    public getFilterText(): string {
        return this.element.getAttribute('data-filter') ?? this.element.getAttribute('data-value') ?? this.element.innerText ?? this.element.innerHTML;
    }

    public getAliases(): string[] {
        return this.element.getAttribute('data-aliases')?.split(',') || [];
    }

    public override shouldShow(filter: string): boolean {
        return this.getFilterText().toLowerCase().includes(filter)
            || this.getAliases().some(alias => alias.toLowerCase().includes(filter));
    }

    public getValue(): string {
        const value = this.element.getAttribute('data-value');
        if (!value) throw new Error('Option does not have a data-value attribute. ' + this.element.outerHTML);
        return value;
    }
}
