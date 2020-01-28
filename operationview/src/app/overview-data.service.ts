import { Injectable } from '@angular/core';
import { TrafficLight } from './model/traffic_light';
import { Transition, States } from './model/transition';
import { Observable, of, timer, Subject } from 'rxjs';
import {take} from 'rxjs/operators';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { KeycloakService} from 'keycloak-angular';

@Injectable({
  providedIn: 'root'
})
export class OverviewDataService {

  private tlList: Subject<TrafficLight[]> = new Subject<TrafficLight[]>();
  private http : HttpClient;

  BASE_URL : string = "https://localhost:8086"

  constructSampleData(): void{

    //Init pedestrian Transitions for pedestrian light
    var pedestrianTransitions : Transition[] = [];
    const reloadInterval = 60;

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
    // tlList.push(new TrafficLight("Pedestrian_SE_FN", "default", 15, pedestrianTransitions, States.RED, 'GOOD', 'SCHEDULED', pedStateList));
    // tlList.push(new TrafficLight("Pedestrian_SE_FW", "default", 15, pedestrianTransitions, States.RED_YELLOW, 'GOOD', 'SCHEDULED', pedStateList));
    // tlList.push(new TrafficLight("Pedestrian_SW_FN", "default", 15, pedestrianTransitions, States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    // tlList.push(new TrafficLight("Pedestrian_SW_FE", "default", 15, pedestrianTransitions, States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    // tlList.push(new TrafficLight("Pedestrian_NE_FS", "default", 15, pedestrianTransitions, States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    // tlList.push(new TrafficLight("Pedestrian_NE_FW", "default", 15, pedestrianTransitions, States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    // tlList.push(new TrafficLight("Pedestrian_NW_FE", "default", 15, pedestrianTransitions, States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    // tlList.push(new TrafficLight("Pedestrian_NW_FS", "default", 15, pedestrianTransitions, States.GREEN, 'GOOD', 'SCHEDULED', pedStateList));
    // tlList.push(new TrafficLight("Car_S", "default", 15, carTransitions, States.YELLOW, 'GOOD', 'SCHEDULED', carStateList));
    // tlList.push(new TrafficLight("Car_E", "default", 15, carTransitions, States.YELLOW_BLINK, 'BROKEN HW', 'MAINTENANCE', carStateList));
    // tlList.push(new TrafficLight("Car_N", "default", 15, carTransitions, States.YELLOW, 'GOOD', 'SCHEDULED', carStateList));
    // tlList.push(new TrafficLight("Car_W", "default", 15, carTransitions, States.RED, 'GOOD', 'SCHEDULED', carStateList));


  }


  constructor(private httpClient: HttpClient, s : KeycloakService) {
    const httpOptions = {
        headers: new HttpHeaders({
          'Authorization': ("Bearer "+s.getToken()),
        })
      };

    this.httpClient.get<TrafficLight[]>(this.BASE_URL+"/lights", httpOptions).subscribe(list =>{
      this.tlList.next(list);
    });
    this.update();
    window.setInterval(fun => {
      this.httpClient.get<TrafficLight[]>(this.BASE_URL+"/lights").subscribe(list =>{
        this.tlList.next(list);
      });
    }, 2000);
  }

  public update() : void {
    this.httpClient.get<TrafficLight[]>(this.BASE_URL+"/lights").subscribe(list =>{
      this.tlList.next(list);
    });
  }

  getTLList() : Subject<TrafficLight[]> {
    return this.tlList;
  }

  getTL(id : number) : Observable<TrafficLight>{
    return this.http.get<TrafficLight>(this.BASE_URL+"/lights/"+id);
  }
}
