import {checkTaskFulfilled, getTasksList} from "./serverRequest";

let fulfilledTasks: string[] = [];

let taskList: {
    name: string;
    description: string;
    difficulty: number;
}[] = [];

export function updateTaskList(lang: string, nodeString: string): void {
    const tasks = getTasksList(lang);
    taskList = [];
    for (const task of tasks) {
        taskList.push(task);
        const {name} = task;

        if (fulfilledTasks.includes(name)) {
            continue;
        }

        const fulfilled = checkTaskFulfilled(lang, name, nodeString);
        if (fulfilled) {
            fulfilledTasks.push(name);
        }
    }
    updateTasksDiv();
}

function updateTasksDiv(): void {
    const tasksDiv = getTasksDiv();
    tasksDiv.innerHTML = "";

    for (const task of taskList) {
        const {name, description, difficulty} = task;
        const taskDiv = document.createElement("div");
        taskDiv.classList.add("task");
        taskDiv.innerHTML = `
            <h3>${"★".repeat(difficulty)} ${name}</h3>
            <p class="description">${description}</p>
        `;
        if (fulfilledTasks.includes(name)) {
            taskDiv.classList.add("fulfilled");
        }
        tasksDiv.appendChild(taskDiv);
    }
}

function getTasksDiv(): HTMLDivElement {
    return document.getElementById("tasks") as HTMLDivElement;
}
