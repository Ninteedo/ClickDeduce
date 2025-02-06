import {AbstractContextMenu} from "./AbstractContextMenu";
import {ContextMenuEntry} from "./ContextMenuEntry";

export class PhantomContextMenu extends AbstractContextMenu {
    constructor(event: MouseEvent) {
        const entries = [
            new ContextMenuEntry("Phantom nodes cannot be interacted with", () => {}, true)
        ]
        super(event, entries);
    }
}
