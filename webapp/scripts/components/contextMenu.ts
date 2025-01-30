import {hasClassOrParentHasClass, parseTreePath} from "../utils";
import {hasCopyCache} from "../actions";
import {Subtree} from "./subtree";
import {addSubtreeEditor} from "./subtreeEditor";
import {addSubtreeToToolbox} from "./SubtreeToolbox";
import {getRootSubtree} from "../treeManipulation";

let contextMenuSelectedElement: HTMLElement | null = null;

/**
 * Opens the context menu on the subtree that was right-clicked.
 * @param e the mouse event
 */
export function openContextMenu(e: MouseEvent): void {
    const highlightElement = getHighlightElementFromEvent(e);

    if (!highlightElement || hasClassOrParentHasClass(highlightElement, 'phantom')) {
        closeContextMenu();
    } else {
        const subtree = getRootSubtree()?.getChildFromPath(parseTreePath(highlightElement.getAttribute('data-tree-path')!));
        if (subtree) {
            e.preventDefault();
            closeContextMenu();
            activeContextMenu = new SubtreeContextMenu(e, subtree);
            contextMenuSelectedElement = highlightElement;
        }
    }
}

/**
 * Closes the context menu.
 */
export function closeContextMenu(): void {
    activeContextMenu?.close();
    activeContextMenu = null;
    clearContextMenuSelectedElement();
}

/**
 * Returns the currently highlighted tree element.
 *
 * If the element is a phantom element, or no element is highlighted, returns null.
 */
export function getHighlightElement(): HTMLElement | null {
    const res = document.querySelector('.highlight');
    if (res instanceof HTMLElement && !hasClassOrParentHasClass(res, 'phantom')) {
        return res;
    }
    return null;
}

function getHighlightElementFromEvent(e: Event): HTMLElement | null {
    let target: EventTarget | null = e.target;
    if (contextMenuSelectedElement !== null) {
        target = null;
    }

    while (target instanceof HTMLElement && !target.classList.contains('highlight')) {
        target = target.parentElement;
    }

    return target instanceof HTMLElement ? target : null;
}

export function getContextMenuSelectedElement(): HTMLElement | null {
    return contextMenuSelectedElement;
}

export function clearContextMenuSelectedElement(): void {
    contextMenuSelectedElement = null;
}

let activeContextMenu: ContextMenu | null = null;

export function getActiveContextMenu(): ContextMenu | null {
    return activeContextMenu;
}

/**
 * A context menu that can be opened on a right-click event.
 */
abstract class ContextMenu {
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
        this.container.classList.add('custom-menu')
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

        console.log(`menuWidth: ${menuWidth}, menuHeight: ${menuHeight}, viewportWidth: ${viewportWidth}, viewportHeight: ${viewportHeight}`);

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

/**
 * An entry in a context menu, which can be clicked to perform an action.
 */
class ContextMenuEntry {
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

/**
 * Context menu for right-clicking on a subtree.
 */
export class SubtreeContextMenu extends ContextMenu {
    private readonly subtree: Subtree;

    public readonly deleteEntry: ContextMenuEntry;
    public readonly copyEntry: ContextMenuEntry;
    public readonly pasteEntry: ContextMenuEntry;
    public readonly editEntry: ContextMenuEntry;
    public readonly sendToToolboxEntry: ContextMenuEntry;

    constructor(event: MouseEvent, subtree: Subtree) {
        const deleteEntry = new ContextMenuEntry("Delete", () => subtree.deleteAction());
        const copyEntry = new ContextMenuEntry("Copy", () => subtree.copyToClipboard(), !hasCopyCache());
        const pasteEntry = new ContextMenuEntry("Paste", () => subtree.pasteAction(), !hasCopyCache());
        const editEntry = new ContextMenuEntry("Edit", () => addSubtreeEditor(subtree));
        const sendToToolboxEntry = new ContextMenuEntry("Send to Toolbox", () => addSubtreeToToolbox(subtree));

        const entries = [
            deleteEntry,
            copyEntry,
            pasteEntry,
            editEntry,
            sendToToolboxEntry
        ];

        super(event, entries);
        this.subtree = subtree;

        this.deleteEntry = deleteEntry;
        this.copyEntry = copyEntry;
        this.pasteEntry = pasteEntry;
        this.editEntry = editEntry;
        this.sendToToolboxEntry = sendToToolboxEntry;
    }

    override close(): void {
        this.subtree.removeHighlight();
        super.close();
    }
}
