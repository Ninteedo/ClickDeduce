import {getErrorDiv} from "../globals/elements";
import {ClassDict} from "../globals/classDict";

let errorTimeoutId: number = 0;

function incrementErrorTimeoutId(): void {
    errorTimeoutId = (errorTimeoutId + 1) % 1000;
}

/**
 * Displays the given error message to the user.
 *
 * Disappears after 5 seconds.
 *
 * @param error the error to display, requires a 'message' property
 */
export function displayError(error: any): void {
    console.error(error);
    const errorDiv = getErrorDiv();
    errorDiv.textContent = error.message;
    errorDiv.classList.add(ClassDict.FADE_IN);
    errorDiv.classList.remove(ClassDict.FADE_OUT);

    incrementErrorTimeoutId();
    let myTimeoutId = errorTimeoutId;
    setTimeout(() => {
        if (myTimeoutId !== errorTimeoutId) return;
        errorDiv.classList.add(ClassDict.FADE_OUT);
        errorDiv.classList.remove(ClassDict.FADE_IN);
    }, 5000);
}
