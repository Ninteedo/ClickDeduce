export function getSelectedMode(): string {
    const selectedRadio = document.querySelector('input[name="mode"]:checked') as HTMLInputElement | null;
    if (selectedRadio) {
        return selectedRadio.value;
    }
    throw new Error("No mode selected");
}

export function getSelectedLanguage(): string {
    const langSelector: HTMLSelectElement = document.getElementById('lang-selector') as HTMLSelectElement;
    return langSelector.value;
}

export function hasClassOrParentHasClass(element: HTMLElement, className: string): boolean {
    return element.classList.contains(className) ||
        (element.parentElement && hasClassOrParentHasClass(element.parentElement, className));
}
