import { Component, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface Log {
  date: string;
  time: string;
  comment: string;
  difficulty: number;
  totalDistance: number;
  totalTime: number;
  rating: number;
  tourID: number;
}
@Component({
  selector: 'app-home',
  imports: [FormsModule],
  templateUrl: './tourlogs.html',
  styleUrl: './tourlogs.css',
})

export class Tourlogs {
  date: Date = new Date();
  comment: string = '';
  difficulty: number = 0;

  //@Input()
  set setDate(date: Date) {
    this.date = date;
  }

  get getDate(): Date {
    return this.date;
  }

  //@Input()
  set setComment(comment: string) {
  this.comment = comment;
  }

  get getComment(): string {
    return this.comment;
  }

  //@Input()
  set setDiff(number: number){
    this.difficulty = number;
  }

}
