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
import {loadTree, redo, resetTreeManipulation, saveTree, undo} from "./treeManipulation";
import {handleTabPressed, resetInterfaceGlobals, zoomToFit} from "./interface";
import panzoom, {PanZoom} from "panzoom";

export let tree: HTMLDivElement;
export let panzoomInstance: PanZoom;

/**
 * Sets up the global variables and initialises the panzoom instance.
 *
 * Can be called again to reset the state of the app
 */
export async function initialise(): Promise<void> {
    resetInterfaceGlobals();
    resetCopyCache();
    await resetTreeManipulation();

    tree = document.getElementById('tree') as HTMLDivElement;

    panzoomInstance = panzoom(tree, {
        bounds: false, boundsPadding: 0, zoomDoubleClickSpeed: 1,
        onTouch: (e) => {
            // TODO: cannot use on mobile currently
            return false;  // tells the library to not preventDefault.
        },
        filterKey: () => {
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
(window as any).saveTree = saveTree;
(window as any).loadTree = loadTree;
