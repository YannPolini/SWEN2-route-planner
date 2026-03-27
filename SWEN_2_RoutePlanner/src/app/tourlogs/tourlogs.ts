import { Component, Input } from '@angular/core';

interface logs {
  date: Date;
  comment: String;
  difficulty: number;
  totalDistance: number;
  totalTime: number;
  rating: number;
  tourID: number;
}

@Component({
  selector: 'app-home',
  imports: [],
  templateUrl: './tourlogs.html',
  styleUrl: './tourlogs.css',
})

export class Tourlogs {
  private date: Date = new Date();
  private comment: String = new String();
  private difficulty: Number = new Number();


  @Input()
  set logDate(date: Date) {
    this.date = date;
  }

  get logDate(): Date {
    return this.date;
  }

  @Input()
  set logComment(comment: String) {
    this.comment = comment;
  }

  get logComment(): String {
    return this.comment;
  }

    @Input()
  set diff(number: Number){
    this.difficulty = number;
  }


}
