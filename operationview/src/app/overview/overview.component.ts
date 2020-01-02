import { Component, OnInit } from '@angular/core';


export interface TrafficLights {
  name: string;
  currentState: string;
  mode: string;
  lastSeen: string;
}

const TL_DATA: TrafficLights[] = [
  {name: "Pedestrian_SE_FN", currentState: 'RED', mode: 'Scheduled', lastSeen: '15s'},
  {name: "Pedestrian_SE_FW", currentState: 'REDYELLOW', mode: 'Scheduled', lastSeen: '27s'},
  {name: "Pedestrian_SW_FN", currentState: 'GREEN', mode: 'Scheduled', lastSeen: '24s'},
  {name: "Pedestrian_SE_FE", currentState: 'GREEN', mode: 'Assigned', lastSeen: '2m'},
  {name: "Pedestrian_NE_FS", currentState: 'GREEN', mode: 'Scheduled', lastSeen: '15s'},
  {name: "Pedestrian_NE_FW", currentState: 'GREEN', mode: 'Scheduled', lastSeen: '15s'},
  {name: "Pedestrian_NW_FS", currentState: 'GREEN', mode: 'Scheduled', lastSeen: '15s'},
  {name: "Pedestrian_NW_FE", currentState: 'GREEN', mode: 'Scheduled', lastSeen: '15s'},
  {name: "Car_S", currentState: 'YELLOW', mode: 'Scheduled', lastSeen: '15s'},
  {name: "Car_E", currentState: 'YELLOW_BLINK', mode: 'Maintenance', lastSeen: '4d'},
  {name: "Car_N", currentState: 'YELLOW', mode: 'Scheduled', lastSeen: '15s'},
  {name: "Car_W", currentState: 'RED_YELLOW', mode: 'Scheduled', lastSeen: '30s'},
];


@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})

export class OverviewComponent implements OnInit {
  title = 'Trafic Light Operation Overview';
  displayedColumns: string[] = ['name', 'currentState', 'mode', 'lastSeen'];
  dataSource = TL_DATA;

  getColorMode(mode : string) : string {
    if (mode === "Maintenance"){
      return "lightsalmon";
    } else if (mode === "Scheduled"){
      return "lightblue";
    } else if (mode === "Assigned") {
      return "lightgreen";
    } else {
      return "grey";
    }
  }

  constructor() { }

  ngOnInit() {
  }

}
