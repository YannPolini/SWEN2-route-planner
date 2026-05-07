import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Tour } from './tour.model';


@Injectable({
  providedIn: 'root'
})

export class TourApiService {
    private apiUrl = 'http://localhost:8080/api/tours';


  constructor(private http: HttpClient) {}

  getAll() {
    console.log("getting all tours");
    return this.http.get<Tour[]>(this.apiUrl);
  }

  create(tour: Tour) {
    console.log("sending", tour);
    return this.http.post(this.apiUrl, tour, {
        responseType: 'text'
    });
  }

  update(tour: Tour) {
    console.log("updating", tour);
    return this.http.put(`${this.apiUrl}/${tour.id}`, tour, {
        responseType: 'text'
    });
  }

  delete(id: string) {
    console.log("delete:" ,id);
    return this.http.delete(`${this.apiUrl}/${id}`, {
        responseType: 'text'
    });
  }
}