import { Injectable } from '@angular/core';
import { Log } from '../tourlogs.model/tourlogs.model';
import { HttpClient } from '@angular/common/http';


@Injectable({
  providedIn: 'root'
})

export class TourLogApiService {
  //private apiUrl = 'http://localhost:8080/all';
    private apiUrl = 'http://localhost:8080/api/tours/1/logs';


  constructor(private http: HttpClient) {}

  getAll() {
    return this.http.get<Log[]>(this.apiUrl);
  }

  create(log: Log) {
    return this.http.post<Log>(this.apiUrl, log);
  }
}