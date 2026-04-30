import { Injectable } from '@angular/core';
import { Log } from './tourlogs.model';
import { HttpClient } from '@angular/common/http';


@Injectable({
  providedIn: 'root'
})

export class TourLogApiService {
  //private apiUrl = 'http://localhost:8080/all';
    private apiUrl = 'http://localhost:8080/api/logs';


  constructor(private http: HttpClient) {}

  getAll() {
    return this.http.get<Log[]>(this.apiUrl);
  }

  create(log: Log) {
    console.log("sending",log);
    return this.http.post(this.apiUrl, log, {
        responseType: 'text'
    });
  }

  update(log: Log) {
    console.log("updating", log);
    return this.http.put(`${this.apiUrl}/${log.logID}`, log, {
        responseType: 'text'
    });
  }

  delete(id: number) {
    console.log("delete:" ,id);
    return this.http.delete(`${this.apiUrl}/${id}`, {
        responseType: 'text'
    });
  }
}