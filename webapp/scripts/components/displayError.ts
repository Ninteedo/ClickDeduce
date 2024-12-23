import {getErrorDiv} from "../globals/elements";

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
    errorDiv.classList.add('fade-in');
    errorDiv.classList.remove('fade-out');

    incrementErrorTimeoutId();
    let myTimeoutId = errorTimeoutId;
    setTimeout(() => {
        if (myTimeoutId !== errorTimeoutId) return;
        errorDiv.classList.add('fade-out');
        errorDiv.classList.remove('fade-in');
    }, 5000);
}
