import {ContextMenuEntry} from "./ContextMenuEntry";
import {ClassDict} from "../../globals/classDict";

/**
 * A context menu that can be opened on a right-click event.
 */
export abstract class AbstractContextMenu {
    protected readonly container: HTMLDivElement;
    private readonly listElement: HTMLUListElement;
    private readonly entries: ContextMenuEntry[];

    /**
     * Creates a new context menu.
     * @param event the right-click event that opened the context menu, used to position the menu
     * @param entries the entries to display in the context menu
     * @protected
     */
    protected constructor(event: MouseEvent, entries: ContextMenuEntry[] = []) {
        this.container = document.createElement('div');
        this.container.id = 'custom-context-menu';
        this.container.classList.add(ClassDict.CUSTOM_MENU)
        this.listElement = document.createElement('ul');
        this.container.appendChild(this.listElement);
        this.entries = entries;
        this.updateContents();

        document.body.appendChild(this.container);

        this.positionMenu(event);
    }

    protected updateContents(): void {
        this.listElement.replaceChildren(...this.entries.map(entry => entry.getElement()));
    }

    /**
     * Closes the context menu, removing it from the DOM.
     */
    close(): void {
        this.container.remove();
    }

    private positionMenu(event: MouseEvent): void {
        this.container.style.display = 'block';

        const menuWidth = this.container.offsetWidth;
        const menuHeight = this.container.offsetHeight;
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;

        let left = event.pageX;
        let top = event.pageY;

        // Prevent overflow on the right
        if (left + menuWidth > viewportWidth) {
            left = viewportWidth - menuWidth - 10;
        }

        // Prevent overflow at the bottom
        if (top + menuHeight > viewportHeight) {
            top = viewportHeight - menuHeight - 10;
        }

        this.container.style.left = `${left}px`;
        this.container.style.top = `${top}px`;
    }
}
