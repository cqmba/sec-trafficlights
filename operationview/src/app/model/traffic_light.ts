import { Transition } from './transition';

export class TrafficLight {
  id : string;
  group: string
  last_seen: number;
  schedule : Transition[];
  state : string;
  health: string;
  mode: string;
  availStates: string[];

  getSchedule() : Transition[]{
    return this.schedule;
  }

  constructor(id: string, group: string, last_seen: number, schedule: Transition[], state: string, health: string, mode: string, availStates: string[]) {
    this.id = id;
    this.group = group;
    this.last_seen = last_seen;
    this.schedule = schedule;
    this.state = state;
    this.health = health;
    this.mode = mode;
    this.availStates = availStates;
  }
}
