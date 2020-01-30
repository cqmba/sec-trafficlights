import { Transition } from './transition';

export class TrafficLight {
  id : number;
  group: number;
  color : string;
  position: string;
  health: string;
  type: string;

  constructor(id: number, group: number, color: string, position: string, health: string, type: string) {
    this.id = id;
    this.group = group;
    this.color = color;
    this.position = position;
    this.health = health;
    this.type = type;
  }
}
