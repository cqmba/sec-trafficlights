import { State } from './state';

export class TrafficLight {
  id : string;
  group: string
  last_seen: number;
  schedule : State[];
  state : State;
  health: string;
  mode: string;
}
