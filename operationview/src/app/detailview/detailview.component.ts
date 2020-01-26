import { Component, OnInit } from '@angular/core';
import { TrafficLight } from '../model/traffic_light';
import { Modes, States, Health, Type } from '../model/transition';
import { OverviewDataService } from '../overview-data.service';
import { Subscription, of as observableOf } from 'rxjs';
import { ActivatedRoute } from '@angular/router';

@Component({
  selector: 'app-detailview',
  templateUrl: './detailview.component.html',
  styleUrls: ['./detailview.component.css']
})
export class DetailviewComponent implements OnInit {

  tl : TrafficLight;
  dataSub : Subscription;
  stateListP : String[];
  stateListV : String[];

  constructor(private dataService: OverviewDataService, private route: ActivatedRoute) {

    this.dataSub = dataService.getTLList().subscribe(tlList => {
      this.tl = tlList.find(x=>x.id === parseInt(this.route.snapshot.paramMap.get('id')));
    });
    this.stateListP = [States.RED, States.GREEN];
    this.stateListV = [States.RED, States.GREEN, States.RED_YELLOW, States.YELLOW, States.YELLOW_BLINK];
  }

  private getListByType() : String[] {
    if(this.tl.type === Type.PEDESTRIAN){
      return this.stateListP;
    } else{
      return this.stateListV;
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

  ngOnInit() {
  }

}
