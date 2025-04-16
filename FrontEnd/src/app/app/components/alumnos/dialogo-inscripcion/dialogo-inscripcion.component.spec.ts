import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DialogoInscripcionComponent } from './dialogo-inscripcion.component';

describe('DialogoInscripcionComponent', () => {
  let component: DialogoInscripcionComponent;
  let fixture: ComponentFixture<DialogoInscripcionComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DialogoInscripcionComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(DialogoInscripcionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
