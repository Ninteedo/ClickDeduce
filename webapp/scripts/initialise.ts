import '../styles/stylesheet.css';
import {
    clearTreeNode,
    copyTreeNode,
    doStartNodeBlank,
    handleLiteralChanged,
    pasteTreeNode,
    resetCopyCache,
    startNodeBlank
} from "./actions";
import {loadTree, redo, resetTreeManipulation, saveTree, undo} from "./treeManipulation";
import {handleTabPressed, resetInterfaceGlobals, zoomToFit} from "./interface";
import panzoom, {PanZoom} from "panzoom";
import {loadImages} from "./imageLoading";

export let tree: HTMLDivElement;
export let panzoomInstance: PanZoom;

/**
 * Sets up the global variables and initialises the panzoom instance.
 *
 * Can be called again to reset the state of the app
 */
export function initialise(skipImages: boolean = false): void {
    resetInterfaceGlobals();
    resetCopyCache();
    if (!skipImages) {
        loadImages();
    }
    resetTreeManipulation();
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

    startNodeBlank();
}

(window as any).initialise = initialise;
(window as any).handleSubmit = doStartNodeBlank;
(window as any).undo = undo;
(window as any).redo = redo;
(window as any).zoomToFit = zoomToFit;
(window as any).handleTabPressed = handleTabPressed;
(window as any).clearTreeNode = clearTreeNode;
(window as any).copyTreeNode = copyTreeNode;
(window as any).pasteTreeNode = pasteTreeNode;
(window as any).handleLiteralChanged = handleLiteralChanged;
(window as any).saveTree = saveTree;
(window as any).loadTree = loadTree;
