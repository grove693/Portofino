import {Component, Input, OnInit} from '@angular/core';
import {HttpClient, HttpHeaders, HttpParams} from '@angular/common/http';
import {PortofinoService} from "../portofino.service";
import {tap} from "rxjs/operators";
import ClassAccessor = portofino.accessors.ClassAccessor;

@Component({
  selector: 'portofino-crud',
  templateUrl: './crud.component.html',
  styleUrls: ['./crud.component.css']
})
export class CrudComponent implements OnInit {

  @Input() config: any;

  classAccessor: ClassAccessor;

  constructor(private http: HttpClient, public portofino: PortofinoService) { }

  ngOnInit() {
    this.http.get<ClassAccessor>(this.portofino.apiPath + this.config.path + '/:classAccessor').subscribe(
      classAccessor => this.classAccessor = classAccessor
    );
    this.http.get<ClassAccessor>(this.portofino.apiPath + 'admin/users').subscribe(
      x => console.log("crud", x),
      e => console.error("error", e)
    );
  }

}