import {handleTabPressed, setNextFocusElement} from "../interface";
import {stripTooltip} from "../utils";
import {AbstractTreeInput} from "./abstractTreeInput";

export class BaseDropdownSelector implements AbstractTreeInput {
    protected readonly container: HTMLDivElement;
    protected readonly input: HTMLInputElement;
    protected readonly dropdown: HTMLDivElement;
    protected readonly options: DropdownOption[];

    protected readonly SELECTOR_FOCUS_CLASS = 'focused';
    protected readonly DROPDOWN_VISIBLE_CLASS = 'show';

    constructor(container: HTMLDivElement, inputSelector: string, dropdownSelector: string, optionsSelector: string) {
        this.container = container;
        this.input = container.querySelector(inputSelector) as HTMLInputElement;
        this.dropdown = container.querySelector(dropdownSelector) as HTMLDivElement;
        this.options = Array.from(this.dropdown.querySelectorAll(optionsSelector))
            .map(option => new DropdownOption(option as HTMLLIElement));

        this.container.classList.add('dropdown-selector-container');
        this.dropdown.classList.add('dropdown');

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
        this.input.addEventListener('keydown', (evt) => this.handleKeydown(evt));
        this.input.addEventListener('focus', () => this.showDropdown());
        this.input.addEventListener('blur', () => this.handleBlur());

        this.dropdown.addEventListener('mousedown', () => this.setInteractingWithDropdown(true));
        this.dropdown.addEventListener('mouseup', () => this.setInteractingWithDropdown(false));
        this.dropdown.addEventListener('wheel', evt => this.handleDropdownWheel(evt));

        this.setupOptionListeners();
    }

    private setupOptionListeners(): void {
        this.options.forEach(option => {
            option.element.addEventListener('click', evt => {
                evt.preventDefault();
                this.selectOption(option);
            });
        });
    }

    protected selectOption(option: DropdownOption): void {
        this.input.value = option.getValue();
        this.input.dispatchEvent(new Event('input'));
        this.hideDropdown();
        this.input.dispatchEvent(new Event('change'));
        this.postSelectOption(option.getValue());
    }

    protected postSelectOption(_value: string): void {}

    protected updateDropdown(): void {
        const filterText = this.input.value.toLowerCase();
        this.options.forEach(option => {
            option.matchesFilter(filterText) ? option.show() : option.hide();
        });
        this.highlightOption(0);
    }

    protected handleKeydown(event: KeyboardEvent): void {
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

    protected handleBlur(): void {
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

    protected showDropdown(): void {
        this.dropdown.classList.add(this.DROPDOWN_VISIBLE_CLASS);
        this.dropdown.scrollTop = 0;
        this.container.classList.add(this.SELECTOR_FOCUS_CLASS);
        this.updateDropdown();
    }

    protected hideDropdown(): void {
        this.dropdown.classList.remove(this.DROPDOWN_VISIBLE_CLASS);
        this.container.classList.remove(this.SELECTOR_FOCUS_CLASS);
    }

    private isDropdownVisible() {
        return this.dropdown.classList.contains(this.DROPDOWN_VISIBLE_CLASS);
    }

    private moveHighlight(offset: number) {
        const options = this.getVisibleOptions();
        const currentIndex = this.getSelectedIndex();
        const newIndex = (currentIndex + offset + options.length) % options.length;
        this.highlightOption(newIndex);
    }

    private highlightOption(index: number) {
        if (!this.isDropdownVisible()) {
            this.showDropdown();
        }

        const options = this.getVisibleOptions();
        options.forEach(option => option.removeHighlight());
        const option = options[index];
        if (option) {
            option.highlight();
        }
    }

    private selectHighlightedOption() {
        const highlighted = this.getSelectedOption();
        if (highlighted) {
            setNextFocusElement(this);
            this.selectOption(highlighted);
        }
    }

    private getVisibleOptions(): DropdownOption[] {
        return this.options.filter((option) => !option.isHidden());
    }

    private getSelectedIndex(): number {
        return this.getVisibleOptions().findIndex((option) => option.isHighlighted());
    }

    private getSelectedOption(): DropdownOption | undefined {
        return this.getVisibleOptions().find(option => option.isHighlighted());
    }

    private handleDropdownWheel(evt: WheelEvent) {
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

class DropdownOption {
    public readonly element: HTMLLIElement;

    protected readonly OPTION_HIDDEN_CLASS = 'hidden';
    protected readonly OPTION_HIGHLIGHT_CLASS = 'highlight';

    constructor(option: HTMLLIElement) {
        this.element = option;
        this.element.querySelectorAll('.tooltip').forEach(tooltipDiv => stripTooltip(tooltipDiv));
    }

    public show(): void {
        this.element.classList.remove(this.OPTION_HIDDEN_CLASS);
    }

    public hide(): void {
        this.element.classList.add(this.OPTION_HIDDEN_CLASS);
    }

    public highlight(): void {
        this.element.classList.add(this.OPTION_HIGHLIGHT_CLASS);
        this.element.scrollIntoView({block: 'nearest'});
    }

    public removeHighlight(): void {
        this.element.classList.remove(this.OPTION_HIGHLIGHT_CLASS);
    }

    public isHidden(): boolean {
        return this.element.classList.contains(this.OPTION_HIDDEN_CLASS);
    }

    public isHighlighted(): boolean {
        return this.element.classList.contains(this.OPTION_HIGHLIGHT_CLASS);
    }

    public getFilterText(): string {
        return this.element.getAttribute('data-filter') ?? this.element.innerText;
    }

    public getAliases(): string[] {
        return this.element.getAttribute('data-aliases')?.split(',') || [];
    }

    public matchesFilter(filter: string): boolean {
        return this.getFilterText().toLowerCase().includes(filter)
            || this.getAliases().some(alias => alias.toLowerCase().includes(filter));
    }

    public getValue(): string {
        const value = this.element.getAttribute('data-value');
        if (!value) throw new Error('Option does not have a data-value attribute. ' + this.element.outerHTML);
        return value;
    }
}
