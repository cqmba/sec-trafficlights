import { TestBed } from '@angular/core/testing';

import { OverviewDataService } from './overview-data.service';

describe('OverviewDataService', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('should be created', () => {
    const service: OverviewDataService = TestBed.get(OverviewDataService);
    expect(service).toBeTruthy();
  });
});
