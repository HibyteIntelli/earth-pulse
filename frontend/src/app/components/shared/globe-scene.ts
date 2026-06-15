import { Component } from '@angular/core';

@Component({
  selector: 'app-globe-scene',
  styleUrl: './globe-scene.css',
  template: `
    <div class="globe" aria-hidden="true">
      <div class="globe__atmo"></div>
      <div class="globe__ball"></div>
      <div class="globe__sphere">
        <div class="globe__spin">
          <i class="lon" style="--i: 0"></i>
          <i class="lon" style="--i: 1"></i>
          <i class="lon" style="--i: 2"></i>
          <i class="lon" style="--i: 3"></i>
          <i class="lon" style="--i: 4"></i>
          <i class="lon" style="--i: 5"></i>
          <span class="pin pin--fire" style="--lon: -58deg; --lat: 28deg; --d: 0s"></span>
          <span class="pin pin--storm" style="--lon: 42deg; --lat: -12deg; --d: 0.7s"></span>
          <span class="pin pin--volcano" style="--lon: 122deg; --lat: -6deg; --d: 1.4s"></span>
          <span class="pin pin--flood" style="--lon: 86deg; --lat: 26deg; --d: 2.1s"></span>
        </div>
        <i class="lat lat--eq" style="--k: 0; --sc: 1"></i>
        <i class="lat" style="--k: -0.5; --sc: 0.866"></i>
        <i class="lat" style="--k: 0.5; --sc: 0.866"></i>
        <i class="lat" style="--k: -0.866; --sc: 0.5"></i>
        <i class="lat" style="--k: 0.866; --sc: 0.5"></i>
      </div>
      <div class="globe__shade"></div>
    </div>
  `,
})
export class GlobeScene {}