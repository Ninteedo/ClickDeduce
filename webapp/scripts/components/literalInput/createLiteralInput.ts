import {LiteralIdentifierLookupInput} from "./literalIdentifierLookupInput";
import {LiteralIntInput} from "./literalIntInput";
import {LiteralBoolInput} from "./literalBoolInput";
import {LiteralInput} from "./literalInput";
import {ClassDict} from "../../globals/classDict";

export function createLiteralInput(input: HTMLInputElement): LiteralInput {
    if (input.classList.contains(ClassDict.IDENTIFIER_LOOKUP)) {
        return new LiteralIdentifierLookupInput(input);
    } else if (input.classList.contains(ClassDict.INTEGER)) {
        return new LiteralIntInput(input);
    } else if (input.type === 'checkbox') {
        return new LiteralBoolInput(input);
    } else {
        return new LiteralInput(input);
    }
}
