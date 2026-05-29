import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Watches } from './watches';

describe('Watches', () => {
  let component: Watches;
  let fixture: ComponentFixture<Watches>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Watches],
    }).compileComponents();

    fixture = TestBed.createComponent(Watches);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
