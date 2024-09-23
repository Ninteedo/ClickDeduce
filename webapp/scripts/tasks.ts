import {checkTaskFulfilled, getTasksList} from "./serverRequest";

let fulfilledTasks: string[] = [];

export interface Task {
    name: string;
    description: string;
    difficulty: number;
}

let taskList: Task[] = [];

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

function createTaskElement({name, description, difficulty}: Task): HTMLDivElement {
    const taskDiv = document.createElement("div");
    taskDiv.classList.add("task");

    const stars = "★".repeat(difficulty);

    taskDiv.innerHTML = `
      <h3>${stars} ${name}</h3>
      <p class="description">${description}</p>
    `;
    if (fulfilledTasks.includes(name)) {
        taskDiv.classList.add("fulfilled");
    }
    return taskDiv;
}

function updateTasksDiv(): void {
    const tasksDiv = getTasksDiv();
    tasksDiv.innerHTML = "";

    if (taskList.length === 0) {
        tasksDiv.classList.add("hidden");
    } else {
        tasksDiv.classList.remove("hidden");
        for (const task of taskList) {
            tasksDiv.appendChild(createTaskElement(task));
        }
    }
}

function getTasksDiv(): HTMLDivElement {
    return document.getElementById("tasks") as HTMLDivElement;
}
