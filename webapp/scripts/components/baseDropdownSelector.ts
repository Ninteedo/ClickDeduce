import {handleTabPressed, setNextFocusElement} from "../interface";

export class BaseDropdownSelector {
    protected readonly container: HTMLDivElement;
    protected readonly input: HTMLInputElement;
    protected readonly dropdown: HTMLDivElement;
    protected readonly options: HTMLLIElement[];

    protected readonly SELECTOR_FOCUS_CLASS = 'focused';
    protected readonly DROPDOWN_VISIBLE_CLASS = 'show';
    protected readonly OPTION_HIDDEN_CLASS = 'hidden';
    protected readonly OPTION_HIGHLIGHT_CLASS = 'highlight';

    constructor(container: HTMLDivElement, inputSelector: string, dropdownSelector: string, optionsSelector: string) {
        this.container = container;
        this.input = container.querySelector(inputSelector) as HTMLInputElement;
        console.log(this.input);
        this.dropdown = container.querySelector(dropdownSelector) as HTMLDivElement;
        this.options = Array.from(this.dropdown.querySelectorAll(optionsSelector)) as HTMLLIElement[];

        this.container.classList.add('dropdown-selector-container');
        this.dropdown.classList.add('dropdown');

        this.setupListeners();
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
            if (!(option instanceof HTMLLIElement)) {
                throw new Error('Option was not an HTMLLIElement');
            }
            option.addEventListener('mousedown', evt => {
                evt.preventDefault();
                this.selectOption(option);
            });
        });
    }

    protected selectOption(option: HTMLLIElement): void {
        this.input.value = option.innerText;
        this.input.dispatchEvent(new Event('input'));
        this.hideDropdown();
        this.input.dispatchEvent(new Event('change'));
    }

    protected updateDropdown(): void {
        const filterText = this.input.value.toLowerCase();
        this.options.forEach(option => {
            const matches = option.innerText.toLowerCase().includes(filterText);
            matches ? this.showOption(option) : this.hideOption(option);
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

    protected showOption(option: HTMLLIElement): void {
        option.classList.remove(this.OPTION_HIDDEN_CLASS);
    }

    protected hideOption(option: HTMLLIElement): void {
        option.classList.add(this.OPTION_HIDDEN_CLASS);
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
        options.forEach((opt) => opt.classList.remove(this.OPTION_HIGHLIGHT_CLASS));
        const option = options[index];
        if (option) {
            option.classList.add(this.OPTION_HIGHLIGHT_CLASS);
            option.scrollIntoView({block: 'nearest'});
        }
    }

    private selectHighlightedOption() {
        const highlighted = this.getSelectedOption();
        if (highlighted) {
            setNextFocusElement(this.input);
            this.selectOption(highlighted);
        }
    }

    private getVisibleOptions(): HTMLLIElement[] {
        return this.options.filter((opt) => !opt.classList.contains(this.OPTION_HIDDEN_CLASS));
    }

    private getSelectedIndex(): number {
        return this.getVisibleOptions().findIndex((opt) =>
            opt.classList.contains(this.OPTION_HIGHLIGHT_CLASS)
        );
    }

    private getSelectedOption(): HTMLLIElement | undefined {
        return this.getVisibleOptions().find((opt) =>
            opt.classList.contains(this.OPTION_HIGHLIGHT_CLASS)
        );
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
