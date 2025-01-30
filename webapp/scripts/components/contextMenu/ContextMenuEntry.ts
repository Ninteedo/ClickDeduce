/**
 * An entry in a context menu, which can be clicked to perform an action.
 */
export class ContextMenuEntry {
    private readonly element: HTMLLIElement;

    /**
     * Creates a new context menu entry.
     * @param text the text to display for the entry
     * @param onClick the function to call when the entry is clicked
     * @param disabled whether the entry should be disabled
     */
    constructor(text: string, onClick: () => void, disabled: boolean = false) {
        this.element = document.createElement('li');
        this.element.classList.add('context-menu-entry');
        this.element.textContent = text;
        this.element.addEventListener('click', onClick);
        if (disabled) {
            this.element.setAttribute('disabled', 'true');
        }
    }

    getElement(): HTMLElement {
        return this.element;
    }

    doClick(): void {
        this.element.click();
    }
}
