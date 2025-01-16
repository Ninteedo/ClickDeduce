import panzoom, {PanZoom} from "panzoom";
import {getTreeContainer} from "../globals/elements";

let panzoomInstance: PanZoom;

/**
 * Zooms the tree to fit the container.
 */
export async function zoomToFit(): Promise<void> {
    const container: HTMLElement = getTreeContainer();
    const rootSubtree: HTMLDivElement = document.querySelector('.subtree[data-tree-path=""]') as HTMLDivElement;

    const scaleWidth = container.clientWidth / rootSubtree.clientWidth;

    panzoomInstance.pause();
    panzoomInstance.moveTo(0, 0);
    panzoomInstance.zoomAbs(0, 0, scaleWidth);
    panzoomInstance.moveTo(0, 0);
    panzoomInstance.resume();
}

/**
 * Returns whether the auto-zoom checkbox is checked.
 */
export function isAutoZoomEnabled(): boolean {
    return getAutoZoomCheckbox().checked;
}

export function setUpPanZoom(tree: HTMLDivElement): void {
    panzoomInstance = panzoom(tree, {
        bounds: true,
        boundsPadding: -0.1,
        zoomDoubleClickSpeed: 1,
        minZoom: 0.1,
        maxZoom: 10,
        onTouch: () => {
            // TODO: cannot use on mobile currently
            return false;  // tells the library to not preventDefault.
        },
        filterKey: () => {
            return true;  // don't let panzoom handle this event:
        }
    });

    getAutoZoomCheckbox().addEventListener('change', () => {
        if (isAutoZoomEnabled()) {
            zoomToFit();
        }
    });
}

function getAutoZoomCheckbox(): HTMLInputElement {
    return document.getElementById('auto-zoom-toggle') as HTMLInputElement;
}

export function centerTree(): void {
    panzoomInstance.moveTo(0, 0);
}
