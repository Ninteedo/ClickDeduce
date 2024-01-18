import '../styles/stylesheet.css';
import {
    clearTreeNode,
    copyTreeNode,
    handleDropdownChange,
    handleLiteralChanged,
    handleSubmit,
    pasteTreeNode,
    resetCopyCache
} from "./actions";
import {redo, resetTreeManipulation, undo} from "./treeManipulation";
import {handleTabPressed, resetInterfaceGlobals, zoomToFit} from "./interface";
import panzoom, {PanZoom} from "panzoom";

export let tree: HTMLDivElement;
export let panzoomInstance: PanZoom;

export async function initialise(): Promise<void> {
    resetInterfaceGlobals();
    resetCopyCache();
    await resetTreeManipulation();

    tree = document.getElementById('tree') as HTMLDivElement;

    panzoomInstance = panzoom(tree, {
        bounds: false, boundsPadding: 0, zoomDoubleClickSpeed: 1,
        onTouch: function (e: Event) {
            // TODO: cannot use on mobile currently
            return false;  // tells the library to not preventDefault.
        },
        filterKey: function (/* e, dx, dy, dz */) {
            return true;  // don't let panzoom handle this event:
        }
    });
}

(window as any).initialise = initialise;
(window as any).handleSubmit = handleSubmit;
(window as any).undo = undo;
(window as any).redo = redo;
(window as any).zoomToFit = zoomToFit;
(window as any).handleTabPressed = handleTabPressed;
(window as any).clearTreeNode = clearTreeNode;
(window as any).copyTreeNode = copyTreeNode;
(window as any).pasteTreeNode = pasteTreeNode;
(window as any).handleDropdownChange = handleDropdownChange;
(window as any).handleLiteralChanged = handleLiteralChanged;
