import { Component } from '@angular/core';
import { httpResource } from '@angular/common/http';
import type { GreetingDto } from '../../api/greetings.dto';

@Component({
  selector: 'app-good-night-world',
  imports: [],
  templateUrl: './good-night-world.html',
  styleUrl: './good-night-world.css',
})
export class GoodNightWorld {
  greeting = httpResource<GreetingDto>(() => '/api/greetings/key/good-night');
}
