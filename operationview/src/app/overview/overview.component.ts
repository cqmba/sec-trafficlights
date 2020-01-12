import { Component, OnInit } from '@angular/core';
import { TrafficLight } from '../model/traffic_light';
import { Modes } from '../model/transition';
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
  displayedColumns: string[] = ['id', 'currentState', 'mode', 'lastSeen'];
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

  constructor( private dataService: OverviewDataService ) {
    this.dataSub = dataService.getTLList().subscribe(tlList => {
      this.tlList = tlList;
    });

  }

  ngOnInit() {
  }

}
