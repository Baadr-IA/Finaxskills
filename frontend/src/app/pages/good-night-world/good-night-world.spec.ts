import { ComponentFixture, TestBed } from '@angular/core/testing';

import { GoodNightWorld } from './good-night-world';

describe('GoodNightWorld', () => {
  let component: GoodNightWorld;
  let fixture: ComponentFixture<GoodNightWorld>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GoodNightWorld],
    }).compileComponents();

    fixture = TestBed.createComponent(GoodNightWorld);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
