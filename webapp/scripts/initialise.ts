import '../styles/main.sass';
import {doStartNodeBlank, exampleLiteralChanged, handleLiteralChanged, resetCopyCache, startNodeBlank} from "./actions";
import {redo, resetTreeManipulation, undo, updateTextInputWidth} from "./treeManipulation";
import {handleTabPressed, resetInterfaceGlobals, toggleControls} from "./interface";
import {loadImages} from "./imageLoading";
import {setupExampleSelector} from "./components/customExprSelector";
import {loadTree, saveTree} from "./saveLoad";
import {
    getExportCloseButton,
    getExportCopyButton,
    getExportLatexButton,
    getLoadButton,
    getRedoButton,
    getSaveButton,
    getShortcutsLink,
    getToggleControlsButton,
    getTree,
    getUndoButton,
    getZoomToFitButton
} from "./globals/elements";
import {setUpPanZoom, zoomToFit} from "./components/panzoom";
import {closeExportOutput, copyExportOutput, exportLaTeX} from "./components/latexOutput";
import {showShortcutsDialog} from "./components/shortcutsModal";
import {setupGuide} from "./guide";

/**
 * Sets up the global variables and initialises the panzoom instance.
 *
 * Can be called again to reset the state of the app
 */
export function initialise(skipImages: boolean = false): void {
    // console.log('initialising');
    resetInterfaceGlobals();
    resetCopyCache();
    if (!skipImages) {
        loadImages();
    }
    resetTreeManipulation();
    setUpPanZoom(getTree());

    setupButtons();
    startNodeBlank();
}

function setupButtons(): void {
    getUndoButton().addEventListener('click', undo);
    getRedoButton().addEventListener('click', redo);
    getZoomToFitButton().addEventListener('click', zoomToFit);
    getSaveButton().addEventListener('click', saveTree);
    getLoadButton().addEventListener('click', loadTree);
    getExportLatexButton().addEventListener('click', exportLaTeX);
    getExportCopyButton().addEventListener('click', copyExportOutput);
    getExportCloseButton().addEventListener('click', closeExportOutput);
    getToggleControlsButton().addEventListener('click', toggleControls);
    getShortcutsLink().addEventListener('click', showShortcutsDialog);
}

(window as any).initialise = initialise;

(window as any).startNodeBlank = doStartNodeBlank;
// (window as any).undo = undo;
// (window as any).redo = redo;
// (window as any).zoomToFit = zoomToFit;
(window as any).handleTabPressed = handleTabPressed;
// (window as any).clearTreeNode = clearTreeNode;
// (window as any).copyTreeNode = copyTreeNode;
// (window as any).pasteTreeNode = pasteTreeNode;
(window as any).handleLiteralChanged = handleLiteralChanged;
// (window as any).saveTree = saveTree;
// (window as any).loadTree = loadTree;
// (window as any).exportLaTeX = exportLaTeX;
// (window as any).copyExportOutput = copyExportOutput;
// (window as any).closeExportOutput = closeExportOutput;
(window as any).setupExampleSelector = setupExampleSelector;
(window as any).exampleLiteralChanged = exampleLiteralChanged;
(window as any).updateTextInputWidth = updateTextInputWidth;
(window as any).setupGuide = setupGuide;
