import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { DetailviewComponent } from '../detailview/detailview.component';
import { OverviewComponent } from '../overview/overview.component';
import { AppAuthGuard } from '../authguard';

const routes: Routes = [
        {
            path: '',
            component: OverviewComponent,
            canActivate: [AppAuthGuard],
        },
        {
          path: 'light/:id',
          component: DetailviewComponent,
          canActivate: [AppAuthGuard],
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
