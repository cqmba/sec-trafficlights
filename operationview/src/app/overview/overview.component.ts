import { Component, OnInit } from '@angular/core';
import { TrafficLight } from '../model/traffic_light';
import { Modes, States, Health } from '../model/transition';
import { OverviewDataService } from '../overview-data.service';
import { Subscription, of as observableOf } from 'rxjs';

@Component({
  selector: 'app-overview',
  templateUrl: './overview.component.html',
  styleUrls: ['./overview.component.css']
})

export class OverviewComponent implements OnInit {

  tlList : TrafficLight[];
  dataSub : Subscription;

  title = 'Trafic Light Operation Overview';
  displayedColumns: string[] = ['position', 'color', 'health', 'type'];
  dataSource = this.tlList;

  getColorMode(mode : string) : string {
    if (mode === Modes.MAINTENANCE){
      return "lightsalmon";
    } else if (mode === Modes.SCHEDULED){
      return "lightblue";
    } else if (mode === Modes.ASSIGNED) {
      return "lightgreen";
    } else {
      return "grey";
    }
  }

  getColorColor(color: string){
    if (color === States.GREEN){
      return "lightgreen";
    } else if (color === States.RED_YELLOW){
      return "lightsalmon";
    } else if (color === States.RED){
      return "#CD5C5C";
    } else if (color === States.YELLOW){
      return "gold";
    } else {
      return "lightsteelblue";
    }
  }

  getColorHealth(color: string){
    if (color === Health.HEALTHY){
      return "mediumaquamarine";
    } else {
      return "salmon";
    }
  }

  constructor( private dataService: OverviewDataService ) {
    this.dataSub = dataService.getTLList().subscribe(tlList => {
      this.tlList = tlList;
    });

  }

  ngOnInit() {
  }

}
