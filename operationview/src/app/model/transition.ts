export class Transition {
  id : string;
  time: number;
  next_state : string;
  entry_state: boolean;

  constructor(id: string, time: number, next_state: string, entry_state: boolean) {
    this.id = id;
    this.time = time;
    this.next_state = next_state;
    this.entry_state = entry_state;
  }
};

export const States = {
  GREEN: "GREEN",
  RED: "RED",
  YELLOW: "YELLOW",
  RED_YELLOW: "RED_YELLOW",
  YELLOW_BLINK: "YELLOW_BLINK"
};

export const Modes = {
  MAINTENANCE: 'MAINTENANCE',
  SCHEDULED: 'SCHEDULED',
  ASSIGNED: 'ASSIGNED',
}
