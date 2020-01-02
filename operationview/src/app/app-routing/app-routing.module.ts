import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DetailviewComponent } from '../detailview/detailview.component';
import { OverviewComponent } from '../overview/overview.component';

const routes: Routes = [
        {
            path: '',
            component: OverviewComponent,
        },
        {
          path: 'detail',
          component: DetailviewComponent,
        }
    ];

    @NgModule({
        imports: [
            RouterModule.forRoot(routes)
        ],
        exports: [
            RouterModule
        ],
        declarations: []
    })
    export class AppRoutingModule { }
