import {Subtree} from "../subtree";
import {hasCopyCache} from "../../actions";
import {addSubtreeEditor} from "../subtreeEditor";
import {addSubtreeToToolbox} from "../SubtreeToolbox";
import {ContextMenuEntry} from "./ContextMenuEntry";
import {AbstractContextMenu} from "./AbstractContextMenu";

/**
 * Context menu for right-clicking on a subtree.
 */
export class SubtreeContextMenu extends AbstractContextMenu {
    private readonly subtree: Subtree;

    public readonly deleteEntry: ContextMenuEntry;
    public readonly copyEntry: ContextMenuEntry;
    public readonly pasteEntry: ContextMenuEntry;
    public readonly editEntry: ContextMenuEntry;
    public readonly sendToToolboxEntry: ContextMenuEntry;

    constructor(event: MouseEvent, subtree: Subtree) {
        const deleteEntry = new ContextMenuEntry("Delete", () => subtree.deleteAction());
        const copyEntry = new ContextMenuEntry("Copy", () => subtree.copyToClipboard());
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
