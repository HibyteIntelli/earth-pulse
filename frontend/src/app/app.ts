import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { CommandNav } from './components/shared/command-nav/command-nav';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, CommandNav],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {}