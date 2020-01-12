import { Component, OnInit } from '@angular/core';
import { TrafficLight } from '../model/traffic_light';
import { Modes } from '../model/transition';
import { OverviewDataService } from '../overview-data.service';
import { Subscription, of as observableOf } from 'rxjs';

@Component({
  selector: 'app-detailview',
  templateUrl: './detailview.component.html',
  styleUrls: ['./detailview.component.css']
})
export class DetailviewComponent implements OnInit {

  constructor() {


  }

  ngOnInit() {
  }

}
