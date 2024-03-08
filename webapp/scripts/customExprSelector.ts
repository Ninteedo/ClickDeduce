import {tree} from "./initialise";
import {hasClassOrParentHasClass} from "./utils";
import {handleExprSelectorChoice} from "./actions";

export function replaceSelectInputs(): void {
    const selectInputs: NodeListOf<HTMLSelectElement> = tree.querySelectorAll(
        'select.expr-dropdown[data-tree-path]:not([disabled]), select.type-dropdown[data-tree-path]:not([disabled])'
    );
    selectInputs.forEach(select => {
        if (hasClassOrParentHasClass(select, 'phantom')) {
            return;
        }

        const options = Array.from(select.options).slice(1);
        const treePath = select.getAttribute('data-tree-path');
        let placeholderText: string;
        let kind: string;
        if (select.classList.contains('expr-dropdown')) {
            placeholderText = 'Enter Expression...';
            kind = 'expr';
        } else {
            placeholderText = 'Enter Type...';
            kind = 'type';
        }
        select.outerHTML =
            `<div class="expr-selector-container" data-tree-path="${treePath}" data-kind="${kind}">
              <input type="text" class="expr-selector-input" placeholder="${placeholderText}" data-tree-path="${treePath}" />
              <button class="expr-selector-button">&#9660;</button>
              <div class="expr-selector-dropdown">
                <ul>
                ${options.map(option => `<li data-value="${option.value}">${option.innerHTML}</li>`).join('')}
                </ul>
              </div>
            </div>`;

        const newSelector = tree.querySelector(`.expr-selector-container[data-tree-path="${treePath}"]`) as HTMLDivElement;
        setupTermSelector(newSelector);
    });
}

export function setupTermSelector(termSelectorContainer: HTMLDivElement): void {
    const input = getSelectorInput(termSelectorContainer);
    const button = getSelectorButton(termSelectorContainer);
    const dropdown = getSelectorDropdown(termSelectorContainer);

    input.addEventListener('input', () => updateExprSelectorDropdown(termSelectorContainer));
    input.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            // selectorEnterPressed(newSelector);
        } else if (event.key === 'ArrowDown') {
            moveSelectorOptionHighlight(termSelectorContainer, 1);
        } else if (event.key === 'ArrowUp') {
            moveSelectorOptionHighlight(termSelectorContainer, -1);
        }
    });
    input.addEventListener('focus', () => showExprSelectorDropdown(termSelectorContainer));
    input.addEventListener('blur', () => hideExprSelectorDropdown(termSelectorContainer));

    button.addEventListener('click', () => input.focus());

    const selectorOptions = Array.from(dropdown.querySelectorAll('ul > li'));
    selectorOptions.forEach(option => {
        if (!(option instanceof HTMLLIElement)) {
            throw new Error('Selector option was not an HTMLLIElement');
        }

        option.addEventListener('mousedown', event => {
            event.preventDefault();
            selectorSelectOption(termSelectorContainer, option)
        });
        option.classList.add('expr-selector-option');
    });
}

export function setupExampleSelector(termSelectorContainer: HTMLDivElement): void {
    const input = getSelectorInput(termSelectorContainer);
    const button = getSelectorButton(termSelectorContainer);
    const dropdown = getSelectorDropdown(termSelectorContainer);
    const output = document.getElementById("expr-selector-output");

    function selectOption(option: HTMLLIElement): void {
        input.value = option.innerText;
        output.textContent = option.textContent;
        hideExprSelectorDropdown(termSelectorContainer);
    }

    input.addEventListener('input', () => updateExprSelectorDropdown(termSelectorContainer));
    input.addEventListener('keydown', (event) => {
        if (event.key === 'Enter') {
            if (isExprSelectorDropdownVisible(termSelectorContainer)) {
                toggleExprSelectorDropdownDisplay(termSelectorContainer);
                return;
            }

            const selectedIndex = getExprSelectorOptionHighlight(termSelectorContainer, false);
            const selectedOption = getSelectorOptions(termSelectorContainer)[selectedIndex];
            if (selectedOption) {
                selectOption(selectedOption);
            }
        } else if (event.key === 'ArrowDown') {
            moveSelectorOptionHighlight(termSelectorContainer, 1);
        } else if (event.key === 'ArrowUp') {
            moveSelectorOptionHighlight(termSelectorContainer, -1);
        }
    });
    input.addEventListener('focus', () => showExprSelectorDropdown(termSelectorContainer));
    input.addEventListener('blur', () => hideExprSelectorDropdown(termSelectorContainer));

    button.addEventListener('click', () => input.focus());

    const selectorOptions = Array.from(dropdown.querySelectorAll('ul > li'));
    selectorOptions.forEach(option => {
        if (!(option instanceof HTMLLIElement)) {
            throw new Error('Selector option was not an HTMLLIElement');
        }

        option.addEventListener('mousedown', event => {
            event.preventDefault();
            selectOption(option);
        });
        option.classList.add('expr-selector-option');
    });
}

function updateExprSelectorDropdown(selectorDiv: HTMLDivElement, keepOpenWhenEmpty: boolean = true) {
    const input = getSelectorInput(selectorDiv);

    if (input.value === '' && !keepOpenWhenEmpty) {
        if (isExprSelectorDropdownVisible(selectorDiv)) {
            toggleExprSelectorDropdownDisplay(selectorDiv);
        }
        return;
    }

    showExprSelectorDropdown(selectorDiv);

    const filterText = input.value.toLowerCase();
    getSelectorOptions(selectorDiv).forEach(option => {
        if (option.innerHTML.toLowerCase().includes(filterText)) {
            showSelectorOption(option);
        } else {
            hideSelectorOption(option);
        }
    });

    setExprSelectorOptionHighlight(selectorDiv, 0);
}

function setExprSelectorOptionHighlight(selectorDiv: HTMLDivElement, highlightIndex: number) {
    const options = getSelectorOptions(selectorDiv);
    options.forEach(option => option.classList.remove('highlight'));
    const filtered = visibleSelectorOptions(selectorDiv);
    if (highlightIndex >= 0 && highlightIndex < filtered.length) {
        filtered[highlightIndex].classList.add('highlight');
    }
}

function getExprSelectorOptionHighlight(selectorDiv: HTMLDivElement, ignoreHidden: boolean): number {
    let options: HTMLLIElement[];
    if (ignoreHidden) {
        options = visibleSelectorOptions(selectorDiv);
    } else {
        options = getSelectorOptions(selectorDiv);
    }
    return options.findIndex(option => option.classList.contains('highlight'));
}

function moveSelectorOptionHighlight(selectorDiv: HTMLDivElement, offset: number): void {
    const filtered = visibleSelectorOptions(selectorDiv);
    const currentHighlightIndex = getExprSelectorOptionHighlight(selectorDiv, true);
    let newHighlightIndex = (currentHighlightIndex + offset) % filtered.length;
    if (newHighlightIndex < 0) newHighlightIndex += filtered.length;  // wrap around from -1 to the end
    setExprSelectorOptionHighlight(selectorDiv, newHighlightIndex);
}

function toggleExprSelectorDropdownDisplay(selectorDiv: HTMLDivElement) {
    if (isExprSelectorDropdownVisible(selectorDiv)) {
        hideExprSelectorDropdown(selectorDiv);
    } else {
        showExprSelectorDropdown(selectorDiv);
    }
}

function showExprSelectorDropdown(selectorDiv: HTMLDivElement) {
    if (isExprSelectorDropdownVisible(selectorDiv)) return;
    getSelectorDropdown(selectorDiv).classList.add('show');
    getSelectorButton(selectorDiv).innerHTML = '&#9650;';
    getSelectorButton(selectorDiv).disabled = true;
    updateExprSelectorDropdown(selectorDiv, true);
}

function hideExprSelectorDropdown(selectorDiv: HTMLDivElement): void {
    if (!isExprSelectorDropdownVisible(selectorDiv)) return;
    getSelectorDropdown(selectorDiv).classList.remove('show');
    getSelectorButton(selectorDiv).innerHTML = '&#9660;';
    getSelectorButton(selectorDiv).disabled = false;
    getSelectorOptions(selectorDiv).forEach(option => {
        option.classList.remove('highlight');
        option.removeAttribute('style');
    });
}

function isExprSelectorDropdownVisible(selectorDiv: HTMLDivElement): boolean {
    return getSelectorDropdown(selectorDiv).classList.contains('show');
}

function showSelectorOption(option: HTMLLIElement): void {
    option.classList.remove('hidden');
}

function hideSelectorOption(option: HTMLLIElement): void {
    option.classList.add('hidden');
}

function isSelectorOptionHidden(option: HTMLLIElement): boolean {
    return option.classList.contains('hidden');
}

function visibleSelectorOptions(selectorDiv: HTMLDivElement): HTMLLIElement[] {
    return getSelectorOptions(selectorDiv).filter(option => !isSelectorOptionHidden(option));
}

function selectorSelectOption(selectorDiv: HTMLDivElement, option: HTMLLIElement): void {
    getSelectorInput(selectorDiv).value = option.innerText;
    handleExprSelectorChoice(selectorDiv, option.getAttribute('data-value'));
    hideExprSelectorDropdown(selectorDiv);
}

export function selectorEnterPressed(selectorDiv: HTMLDivElement): void {
    if (!isExprSelectorDropdownVisible(selectorDiv)) {
        toggleExprSelectorDropdownDisplay(selectorDiv);
        return;
    }

    const selectedIndex = getExprSelectorOptionHighlight(selectorDiv, false);
    const selectedOption = getSelectorOptions(selectorDiv)[selectedIndex];
    if (selectedOption) {
        selectorSelectOption(selectorDiv, selectedOption);
    }
}

function getSelectorInput(selectorDiv: HTMLDivElement): HTMLInputElement {
    return selectorDiv.querySelector('.expr-selector-input') as HTMLInputElement;
}

function getSelectorButton(selectorDiv: HTMLDivElement): HTMLButtonElement {
    return selectorDiv.querySelector('.expr-selector-button') as HTMLButtonElement;
}

function getSelectorDropdown(selectorDiv: HTMLDivElement): HTMLDivElement {
    return selectorDiv.querySelector('.expr-selector-dropdown') as HTMLDivElement;
}

function getSelectorOptions(selectorDiv: HTMLDivElement): HTMLLIElement[] {
    const dropdown = selectorDiv.querySelector('.expr-selector-dropdown') as HTMLDivElement;
    return Array.from(dropdown.querySelectorAll('ul > li'));
}
