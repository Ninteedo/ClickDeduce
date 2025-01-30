import {lastNodeString} from "../treeManipulation";

/**
 * Finds the substring of the node string at the given path.
 * @param path the tree path to the node, integers separated by dashes
 * @param nodeString starting node string, uses lastNodeString if not provided
 */
export function getNodeStringFromPath(path: string, nodeString: string | undefined = undefined): string {
    /**
     * Parses the node string and returns the arguments of the given node.
     * <p>
     * The node string is the name followed by a comma-separated list of arguments in parentheses.
     * The arguments can themselves be nodes, so a recursive approach is used.
     * </p>
     * <p>
     * There can also be string literals in the arguments
     * which can contain commas, and parentheses, and escape characters.
     * </p>
     * <p>
     * For example, the node string <code>'Plus(Num("1"), Times(Num(""), Num("mess(\")\\")))'</code>
     * would return <code>['Num("1")', 'Times(Num(""), Num("mess(\")\\"))']</code>.
     * </p>
     *
     * @param node
     */
    function nodeArgs(node: string): string[] {
        let current: string = '';
        let nodes: string[] = [];
        let depth: number = 0;
        let escaped: boolean = false;
        let inString: boolean = false;
        for (let char of node) {
            if (escaped) {
                current += "\\" + char;
                escaped = false;
            } else if (char === '\\') {
                escaped = true;
            } else if (char === '(' && !inString) {
                if (depth === 0) {
                    current = '';
                } else {
                    current += char;
                }
                depth += 1;
            } else if (char === ')' && !inString) {
                depth -= 1;
                if (depth === 0) {
                    nodes.push(current);
                } else {
                    current += char;
                }
            } else if (char === ',' && depth === 1 && !inString) {
                nodes.push(current);
                current = '';
            } else if (char === '"' && !escaped) {
                inString = !inString;
                current += char;
            } else {
                current += char;
            }
        }
        return nodes;
    }

    function recurse(curr: string, remaining: number[]): string {
        if (remaining.length === 0) {
            return curr;
        }

        const next = remaining.shift();
        if (next === undefined) {
            throw new Error('Unexpected undefined value');
        }

        const nodeArgsList = nodeArgs(curr)[1];
        const innerNode = nodeArgs(nodeArgsList)[next];
        const nextNodeString = nodeArgs(innerNode)[0];
        return recurse(nextNodeString, remaining);
    }

    if (!nodeString) {
        if (!lastNodeString) {
            throw new Error('No node string to get path from');
        }
        nodeString = lastNodeString;
    }
    return recurse(nodeString, path.split('-').map(s => parseInt(s)).filter(n => !isNaN(n)));
}
