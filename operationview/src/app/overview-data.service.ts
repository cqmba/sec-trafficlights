import { Injectable } from '@angular/core';
import { TrafficLight } from './model/traffic_light';
import { Transition, States } from './model/transition';
import { Observable, of } from 'rxjs';
import {Subject} from "rxjs";

@Injectable({
  providedIn: 'root'
})
export class OverviewDataService {

  private tlList: TrafficLight[];
  private tlListFlat : TrafficLight[];

  constructSampleData(): void{

    //Init pedestrian Transitions for pedestrian light
    var pedestrianTransitions : Transition[] = [];

    var pedStateRED  = new Transition( States.RED, 20, States.GREEN, true);
    var pedStateGREEN  = new Transition(States.GREEN, 10, States.RED, false);

    pedestrianTransitions.push(pedStateRED);
    pedestrianTransitions.push(pedStateGREEN);

    //Init car Transitions
    var carTransitions: Transition[] = [];

    var carStateRED  = new Transition( States.RED, 10, States.RED_YELLOW, true);
    var carStateREDYELLOW = new Transition( States.RED_YELLOW, 10, States.GREEN, true);
    var carStateGREEN  = new Transition(States.GREEN, 15, States.YELLOW, false);
    var carStateYELLOW = new Transition( States.YELLOW, 10, States.RED, true);
    var carStateYELLOWBLINK = new Transition( States.YELLOW_BLINK, 0, States.RED, true);

    carTransitions.push(carStateRED);
    carTransitions.push(carStateREDYELLOW);
    carTransitions.push(carStateGREEN);
    carTransitions.push(carStateYELLOW);
    carTransitions.push(carStateYELLOWBLINK);

    //Init State Lists
    var pedStateList = [ States.GREEN, States.RED];
    var carStateList = [ States.RED, States.RED_YELLOW, States.GREEN, States.YELLOW, States.YELLOW_BLINK];
    var tlList: TrafficLight[] = [];
    //Init trafic lights
    tlList.push(new TrafficLight("Pedestrian_SE_FN", "default", 15, [], States.RED, 'GOOD', 'SCHEDULED', pedStateList));
    tlList.push(new TrafficLight("Pedestrian_SE_FW", "default", 15, [], States.RED_YELLOW, 'GOOD', 'SCHEDULED', pedStateList));
    tlList.push(new TrafficLight("Pedestrian_SW_FN", "default", 15, [], States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    tlList.push(new TrafficLight("Pedestrian_SW_FE", "default", 15, [], States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    tlList.push(new TrafficLight("Pedestrian_NE_FS", "default", 15, [], States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    tlList.push(new TrafficLight("Pedestrian_NE_FW", "default", 15, [], States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    tlList.push(new TrafficLight("Pedestrian_NW_FE", "default", 15, [], States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    tlList.push(new TrafficLight("Pedestrian_NW_FS", "default", 15, [], States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    tlList.push(new TrafficLight("Car_S", "default", 15, carTransitions, States.YELLOW, 'GOOD', 'SCHEDULED', carStateList));
    tlList.push(new TrafficLight("Car_E", "default", 15, carTransitions, States.YELLOW_BLINK, 'BROKEN HW', 'MAINTENANCE', carStateList));
    tlList.push(new TrafficLight("Car_N", "default", 15, carTransitions, States.YELLOW, 'GOOD', 'SCHEDULED', carStateList));
    tlList.push(new TrafficLight("Car_W", "default", 15, carTransitions, States.RED, 'GOOD', 'SCHEDULED', carStateList));

    this.tlList = tlList;
  }


  constructor() {
    this.constructSampleData();
  }

  getTLList() : Observable<TrafficLight[]> {
    return of (this.tlList);
  }

  getTL(id : string) : TrafficLight{
    for (var tl of this.tlListFlat){
      if (tl.id === id){
        return tl;
      }
    }
  }
}
