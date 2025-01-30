import {AbstractContextMenu} from "./AbstractContextMenu";
import {ContextMenuEntry} from "./ContextMenuEntry";
import {getSubtreeToolbox, ToolboxEntry} from "../SubtreeToolbox";

export class ToolboxContextMenu extends AbstractContextMenu {
    private readonly toolboxEntry: ToolboxEntry;

    constructor(event: MouseEvent, toolboxEntry: ToolboxEntry) {
        const entries = [
            new ContextMenuEntry("Delete", () => getSubtreeToolbox().removeEntry(toolboxEntry))
        ];

        super(event, entries);

        this.toolboxEntry = toolboxEntry;
    }

    getToolboxEntry(): ToolboxEntry {
        return this.toolboxEntry;
    }
}
