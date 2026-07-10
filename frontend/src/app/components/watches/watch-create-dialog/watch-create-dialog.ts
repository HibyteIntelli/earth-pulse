import { Component, inject, signal } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { MapStateService } from '../../map/map-state.service';
import { WatchService } from '../../../core/watch/watch.service';
import { WatchFormValue, WatchRequest } from '../../../core/watch/watch.models';
import { watchErrorMessage } from '../../../core/watch/watch-errors';
import { WatchForm } from '../watch-form/watch-form';

@Component({
  selector: 'app-watch-create-dialog',
  imports: [WatchForm],
  templateUrl: './watch-create-dialog.html',
  styleUrl: './watch-create-dialog.css',
  host: {
    '(document:keydown.escape)': 'onCancel()',
  },
})
export class WatchCreateDialog {
  private readonly mapState = inject(MapStateService);
  private readonly watches = inject(WatchService);

  protected readonly region = this.mapState.pendingWatchRegion;
  protected readonly pending = signal(false);
  protected readonly error = signal<string | null>(null);

  protected onSave(value: WatchFormValue): void {
    const body: WatchRequest = {
      name: value.name,
      minLat: value.region.minLat,
      maxLat: value.region.maxLat,
      minLon: value.region.minLon,
      maxLon: value.region.maxLon,
      categories: value.categories,
      digestMode: value.digestMode,
      readingLevel: value.readingLevel,
    };
    this.pending.set(true);
    this.error.set(null);
    this.watches.create(body).subscribe({
      next: () => {
        this.pending.set(false);
        this.mapState.cancelDrawing();
        this.mapState.flashWatchCreated();
      },
      error: (err: HttpErrorResponse) => {
        this.pending.set(false);
        this.error.set(watchErrorMessage(err));
      },
    });
  }

  protected onCancel(): void {
    if (this.region()) this.mapState.cancelDrawing();
  }
}